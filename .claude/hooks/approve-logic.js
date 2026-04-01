#!/usr/bin/env node
// Taken from: https://github.com/anthropics/claude-code/issues/27957#issuecomment-4126956729
const fs = require('fs');
const path = require('path');

const HOME = process.env.HOME || process.env.USERPROFILE;
const LOG_FILE = path.join(HOME, '.claude/hooks/debug.log');

function log(message) {
    const timestamp = new Date().toISOString();
    fs.appendFileSync(LOG_FILE, `[${timestamp}] ${message}\n`);
}

function readJsonSafe(filePath) {
    try {
        if (fs.existsSync(filePath)) {
            return JSON.parse(fs.readFileSync(filePath, 'utf8'));
        }
    } catch (e) {
        log(`Warning: Could not parse ${filePath}: ${e.message}`);
    }
    return null;
}

function collectAllowLists() {
    const allowRules = [];

    // Global settings: ~/.claude/settings.json
    const globalSettings = readJsonSafe(path.join(HOME, '.claude/settings.json'));
    if (globalSettings?.permissions?.allow) {
        allowRules.push(...globalSettings.permissions.allow);
    }

    // Global local settings: ~/.claude/settings.local.json
    const globalLocal = readJsonSafe(path.join(HOME, '.claude/settings.local.json'));
    if (globalLocal?.permissions?.allow) {
        allowRules.push(...globalLocal.permissions.allow);
    }

    // Project-level settings: .claude/settings.json and .claude/settings.local.json in CWD
    const cwd = process.cwd();
    const projectSettings = readJsonSafe(path.join(cwd, '.claude/settings.json'));
    if (projectSettings?.permissions?.allow) {
        allowRules.push(...projectSettings.permissions.allow);
    }
    const projectLocal = readJsonSafe(path.join(cwd, '.claude/settings.local.json'));
    if (projectLocal?.permissions?.allow) {
        allowRules.push(...projectLocal.permissions.allow);
    }

    return allowRules;
}

async function main() {
    try {
        const inputData = fs.readFileSync(0, 'utf8');
        if (!inputData) return;

        const { tool_name, tool_input } = JSON.parse(inputData);
        if (tool_name !== 'Bash' || !tool_input.command) return;

        const currentCmd = tool_input.command;

        log(`--- New Check ---`);
        log(`Command: ${currentCmd}`);

        const allowList = collectAllowLists();
        log(`Allow rules found: ${JSON.stringify(allowList)}`);

        const patterns = allowList
            .filter(item => item.startsWith('Bash('))
            .map(item => {
                let raw = item.replace(/^Bash\((.*)\)$/, '$1').replace(/:\*$/, '');
                const escaped = raw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/\\\*/g, '.*');
                return {
                    original: item,
                    regex: new RegExp(`^${escaped}`)
                };
            });

        let matchedRule = null;
        const isApproved = patterns.some(p => {
            if (p.regex.test(currentCmd)) {
                matchedRule = p.original;
                return true;
            }
            return false;
        });

        if (isApproved) {
            log(`MATCHED: [${matchedRule}] -> Auto-approving.`);
            console.log(JSON.stringify({ decision: "approve" }));
        } else {
            log(`NO MATCH: Command did not match any allowlist patterns.`);
        }

    } catch (e) {
        log(`CRITICAL ERROR: ${e.message}`);
        process.exit(0);
    }
}

main();
