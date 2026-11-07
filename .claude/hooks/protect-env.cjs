const { readFileSync } = require('node:fs');

const { tool_input: input } = JSON.parse(readFileSync(0, 'utf8'));
const fileName = input?.file_path?.split(/[\\/]/).pop();

if (fileName?.toLowerCase() === '.env') {
  console.error('Edit and Write are blocked for .env. Configure local credentials manually.');
  process.exitCode = 2;
}
