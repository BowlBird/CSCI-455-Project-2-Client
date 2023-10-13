import java.io.EOFException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.jvm.Throws

/**
 * Handles TCP Connections by automatically reading objects from
 * the connection passed in.
 *
 * Also allows sending Objects back to server.
 */
private class Connection<T>(private val socket: Socket, private val  onReceive: () -> Unit = {}, private val onEndpointDisconnect: () -> Unit = {}) {

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
    var messages: List<T> = listOf()
        private set

    /**
     * Pops the oldest message from the message list
     * and returns it
     */
    fun popMessage() = _messages.removeAt(0)


    /**
     * Start listening for messages received from the server.
     * NOTE: if the messages received are not of Type <T>, no messages
     * will ever be received!
     */
    @Throws(ClassCastException::class)
    private fun receiveMessages() {
        //get stream from server
        val inputStream = ObjectInputStream(socket.getInputStream())
        while(receiveMessages) {
            //wrapped in try on the chance the endpoint disconnects randomly
            try {
                //read the object
                with(inputStream.readObject()) {
                    try {
                        //try to cast and add to internal message list
                        _messages += this as T

                        //induce callback
                        onReceive()
                    } catch (e: ClassCastException) {
                        //rethrow exception if cast fails
                        throw ClassCastException("Connection received incorrect type from server!")
                    }
                }
            } catch(e: EOFException) {
                onEndpointDisconnect()
            }
        }
    }

    /**
     * Sends a message to the connection endpoint of type T
     */
    fun sendMessage(message: T) {
        val outputStream = ObjectOutputStream(socket.getOutputStream())
        outputStream.writeObject(message)
    }
}

/**
 * Manager for Connections
 * Holds logic to only allow one connection at a time, as well as a hook to all signals (onReceive)
 *
 * Ensure that you call dispose before removing this object.
 */
class ConnectionRepository<T>(var onReceive: () -> Unit = {}) {
    // === vars ===
    private var connection: Connection<T>? = null
    val isConnected: Boolean
        get() = connection != null

    var messages: List<T> = listOf()
        private set

    /**
     * With ip and port, overwrite any open connection with the new connection.
     * Before the overwrite however, the socket will try to open. If it is able to open, and the rest of the
     * connection goes through, return true, else return false
     */
    fun openConnection(ip: String, port: Int): Boolean {
        // try to open the socket
        val socket = try {
            Socket(ip, port)
        } catch(_: Exception) {
            return false
        }

        //ensure there is only one connection
        closeConnection()

        //create new connection
        connection = Connection(
            socket = socket,
            onReceive = {
                //setup its own callback, as well as users callback
                connection?.messages?.size?.let {
                    repeat (it) {
                        connection?.popMessage()?.let { message ->
                            messages += message
                        }
                    }
                }
                onReceive()
            },
            onEndpointDisconnect = {
                //handles if the server randomly disconnects
                connection?.interrupt()
                connection = null
            }
        )

        return true
    }

    /**
     * dispose of connection socket, and allow for garbage collection
     */
    fun closeConnection() {
        connection?.interrupt()
        connection = null
    }

    /**
     * wrapper for close connection to be more idiomatic
     */
    fun dispose() = closeConnection()
}