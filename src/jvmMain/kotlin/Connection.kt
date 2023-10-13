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
class Connection<T>(private val socket: Socket, private val  onReceive: () -> Unit = {}) {

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
     * Start listening for messages received from the server.
     * NOTE: if the messages received are not of Type <T>, no messages
     * will ever be received!
     */
    @Throws(ClassCastException::class)
    private fun receiveMessages() {
        //get stream from server
        val inputStream = ObjectInputStream(socket.getInputStream())
        while(receiveMessages) {
            //read the object
            with(inputStream.readObject()) {
                try {
                    //try to cast and add to internal message list
                    _messages += this as T
                } catch (e: ClassCastException) {
                    //rethrow exception if cast fails
                    throw ClassCastException("Connection received incorrect type from server!")
                }
            }
            //induce callback
            onReceive()
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