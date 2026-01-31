const http = require('http');

const options = {
    hostname: '127.0.0.1',
    port: 8090,
    path: '/models/list',
    method: 'POST',
    headers: {
        'Content-Type': 'application/json'
    }
};

const req = http.request(options, (res) => {
    console.log(`STATUS: ${res.statusCode}`);
    res.setEncoding('utf8');
    res.on('data', (chunk) => {
        console.log(`BODY: ${chunk}`);
    });
    res.on('end', () => {
        console.log('No more data in response.');
    });
});

req.on('error', (e) => {
    console.error('problem with request:', e);
});

// Write data to request body
req.write(JSON.stringify({ pageNum: 1, pageSize: 10 }));
req.end();
