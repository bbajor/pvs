// Test script for Whisper container
// Run with: node test-whisper.js

const http = require('http');
const fs = require('fs');

const WHISPER_HOST = process.env.WHISPER_HOST || 'localhost';
const WHISPER_PORT = process.env.WHISPER_PORT || '8000';

console.log(`Testing Whisper container at ${WHISPER_HOST}:${WHISPER_PORT}`);

// Test 1: Health check
console.log('\n=== Test 1: Health Check ===');
const healthUrl = `http://${WHISPER_HOST}:${WHISPER_PORT}/health`;
http.get(healthUrl, (res) => {
    console.log(`Health check status: ${res.statusCode}`);
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
        console.log(`Response: ${data}`);
        if (res.statusCode === 200) {
            console.log('✓ Health check passed\n');
            testTranscription();
        } else {
            console.log('✗ Health check failed\n');
            console.log('Please check if Whisper container is running:');
            console.log('  docker ps | grep whisper');
        }
    });
}).on('error', (err) => {
    console.error('✗ Health check error:', err.message);
    console.log('\nPlease check if Whisper container is running:');
    console.log('  docker ps | grep whisper');
});

function testTranscription() {
    console.log('=== Test 2: Transcription (requires audio file) ===');
    console.log('To test transcription, create a small audio file and run:');
    console.log('  curl -X POST -F "audio=@test.webm" http://' + WHISPER_HOST + ':' + WHISPER_PORT + '/transcribe');
    console.log('\nExample with ffmpeg to create test file:');
    console.log('  ffmpeg -f lavfi -i "sine=frequency=1000:duration=5" -acodec libopus test.webm');
}

