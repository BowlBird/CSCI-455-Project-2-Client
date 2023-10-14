import java.io.EOFException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ConnectException
import java.net.Socket
import java.net.SocketException
import kotlin.concurrent.thread
import kotlin.jvm.Throws

/**
 * Handles TCP Connections by automatically reading objects from
 * the connection passed in.
 *
 * Also allows sending Objects back to server.
 */
private class Connection<T>(private val socket: Socket, private val  onReceive: () -> Unit = {}, private val onEndpointDisconnect: () -> Unit = {}) {
    // Object output Stream must be initialized before Object Input Stream
    val output = ObjectOutputStream(socket.getOutputStream())
    val input = ObjectInputStream(socket.getInputStream())

    // === Logic for Messages Thread ===
    private var receiveMessages = true
    init { thread { receiveMessages() } }

    /**
     * Call when ending the connection.
     * If this is ignored, the socket will never be closed
     * and messages will continue to be received.
     */
    fun interrupt() {
        receiveMessages = false
        socket.close()
    }

    // private mutable list of messages
    private val _messages = mutableListOf<T>()

    /**
     * Receive a list of messages returned from the server.
     * Messages are sorted in chronological order, with new messages
     * at the end of the list.
     */
    val messages: List<T>
        get() = _messages

    /**
     * Pops the oldest message from the message list
     * and returns it
     */
    fun popMessage() = _messages.removeAt(0)

    /**
     * Returns IP address that you are connected to
     */
    val connection: String
        get() = socket.inetAddress.hostAddress

    /**
     * Start listening for messages received from the server.
     * NOTE: if the messages received are not of Type <T>, no messages
     * will ever be received!
     */
    @Throws(ClassCastException::class)
    private fun receiveMessages() {
        println("Started Listening for Messages from Endpoint.")
        while(receiveMessages) {
            //wrapped in try on the chance the endpoint disconnects randomly
            try {
                //read the object
                with(input.readObject()) {
                    try {
                        //try to cast and add to internal message list
                        val message = this as T
                        _messages += message
                        println("Successfully received '$message' from Endpoint.")

                        //induce callback
                        onReceive()
                    } catch (e: ClassCastException) {
                        //rethrow exception if cast fails
                        throw ClassCastException("Connection received incorrect type from server!")
                    }
                }
            } catch(e :Exception) {
                //we can ignore if socket closed as that means we closed it!
                if (e.message != "Socket closed") {
                    println("Socket Connection Unexpectedly Closed; Closing Connection.")
                    onEndpointDisconnect()
                }
            }
        }
    }

    /**
     * Sends a message to the connection endpoint of type T
     */
    fun sendMessage(message: T): Boolean {
        return try {
            output.writeObject(message)
            output.flush()
            println("Successfully sent '$message' to Endpoint")
            true
        } catch( e: Exception) {
            println("Failed to send '$message' to Endpoint")
            false
        }
    }
}

/**
 * Manager for Connections
 * Holds logic to only allow one connection at a time, as well as a hook to all signals (onReceive)
 *
 * Ensure that you call dispose before removing this object.
 */
class ConnectionRepository<T>(
    var onReceive: () -> Unit = {},
    var onEndpointDisconnect: () -> Unit = {},
    var onConnect: () -> Unit = {}
) {
    // === vars ===
    private var _connection: Connection<T>? = null
    val isConnected: Boolean
        get() = _connection != null

    var messages: List<T> = listOf()
        private set

    val connection: String?
        get() = _connection?.connection

    /**
     * With ip and port, overwrite any open connection with the new connection.
     * Before the overwrite however, the socket will try to open. If it is able to open, and the rest of the
     * connection goes through, return true, else return false
     *
     */
    fun openConnection(ip: String, port: Int) =
        thread {
            // try to open the socket
            val socket = try {
                //need some logic to handle if a connection takes too long to connect
                val currentThread = Thread.currentThread()
                //will interrupt this thread if it takes too long
                val timerThread = thread {
                    Thread.sleep(500)
                    if (currentThread.isAlive) {
                        currentThread.interrupt()
                        println("Server Connection Interrupted. Timeout.")
                    }
                }
                //try to open socket
                val socket = try {
                    Socket(ip, port)
                } catch (e: ConnectException) {
                    println("Connection with '$ip' on Port '$port' Refused.")
                    null //return
                }
                socket //return
            } catch (_: Exception) {
                null //return
            }


            if (socket != null) {
                println("Successfully Opened Socket Connection with '${socket.inetAddress.hostAddress}' on Port '${socket.port}'")
                //ensure there is only one connection
                closeConnection()
                //create new connection
                _connection = Connection(
                    socket = socket,
                    onReceive = {
                        //setup its own callback, as well as users callback
                        _connection?.messages?.size?.let {
                            repeat(it) {
                                _connection?.popMessage()?.let { message ->
                                    messages += message
                                }
                            }
                        }
                        onReceive()
                    },
                    onEndpointDisconnect = {
                        //handles if the server randomly disconnects
                        closeConnection()
                        onEndpointDisconnect()
                    }
                )
                onConnect()
            }
        }

    /**
     * dispose of connection socket, and allow for garbage collection
     */
    fun closeConnection() {
        if (_connection != null) {
            println("Disposing Previous Connection")
            _connection?.interrupt()
            _connection = null
        }
    }

    /**
     * wrapper for close connection to be more idiomatic
     */
    fun dispose() {
        println("Disposing Connection Repository")
        closeConnection()
    }

    /**
     * will send a message and return true if it was sent
     * If for example _connection is null, this will return false.
     */
    fun sendMessage(message: T): Boolean {
        println("Passing message underlying connection...")
        return _connection?.sendMessage(message) ?: false
    }
}