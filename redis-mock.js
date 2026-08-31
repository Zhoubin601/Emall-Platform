const net = require('net')

const store = new Map()

function parseResp(data) {
  const str = data.toString()
  const lines = str.split('\r\n').filter(Boolean)
  const args = []
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].startsWith('*') || lines[i].startsWith('$')) continue
    args.push(lines[i])
  }
  return args
}

const server = net.createServer(socket => {
  socket.on('error', () => {})
  let authenticated = true

  socket.on('data', data => {
    try {
      const args = parseResp(data)
      if (args.length === 0) return

      const cmd = args[0].toUpperCase()

      if (cmd === 'PING') {
        socket.write('+PONG\r\n')
      } else if (cmd === 'AUTH') {
        socket.write('+OK\r\n')
      } else if (cmd === 'COMMAND') {
        socket.write('*0\r\n')
      } else if (cmd === 'CLIENT') {
        socket.write('+OK\r\n')
      } else if (cmd === 'SELECT') {
        socket.write('+OK\r\n')
      } else if (cmd === 'INFO') {
        const info = 'redis_version:7.0.0\r\n'
        socket.write(`$${info.length}\r\n${info}\r\n`)
      } else if (cmd === 'SET') {
        const key = args[1]
        const val = args[2]
        // Check NX option
        const isNx = args.some(a => a.toUpperCase() === 'NX')
        if (isNx && store.has(key)) {
          socket.write('$-1\r\n')
        } else {
          store.set(key, val)
          socket.write('+OK\r\n')
        }
      } else if (cmd === 'SETNX') {
        const key = args[1]
        const val = args[2]
        if (store.has(key)) {
          socket.write(':0\r\n')
        } else {
          store.set(key, val)
          socket.write(':1\r\n')
        }
      } else if (cmd === 'GET') {
        const key = args[1]
        if (store.has(key)) {
          const val = store.get(key)
          const buf = Buffer.from(val)
          socket.write(`$${buf.length}\r\n${val}\r\n`)
        } else {
          socket.write('$-1\r\n')
        }
      } else if (cmd === 'DEL') {
        let count = 0
        for (let i = 1; i < args.length; i++) {
          if (store.delete(args[i])) count++
        }
        socket.write(`:${count}\r\n`)
      } else if (cmd === 'EXISTS') {
        const key = args[1]
        socket.write(store.has(key) ? ':1\r\n' : ':0\r\n')
      } else if (cmd === 'KEYS') {
        const keys = Array.from(store.keys())
        let resp = `*${keys.length}\r\n`
        for (const k of keys) {
          const buf = Buffer.from(k)
          resp += `$${buf.length}\r\n${k}\r\n`
        }
        socket.write(resp)
      } else if (cmd === 'EVAL' || cmd === 'EVALSHA') {
        socket.write('+OK\r\n')
      } else if (cmd === 'QUIT') {
        socket.write('+OK\r\n')
        socket.end()
      } else {
        socket.write('+OK\r\n')
      }
    } catch (err) {
      socket.write('-ERR ' + err.message + '\r\n')
    }
  })
})

server.listen(6379, '127.0.0.1', () => {
  console.log('In-memory Redis RESP server listening on port 6379')
})
