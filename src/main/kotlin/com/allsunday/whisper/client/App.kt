package com.allsunday.whisper.client

import com.allsunday.whisper.getLoginNotification
import com.allsunday.whisper.getLogoffNotification
import com.allsunday.whisper.getTalkToPeerNotification
import com.allsunday.whisper.proto.*
import com.allsunday.whisper.toMessage
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.geometry.Pos
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import tornadofx.*


class MyApp : App(MyView::class)


class MyView : View() {
    val controller: MyController by inject()

    override val root = borderpane {
        top {
            padding = insets(10)
            label("title")
        }

        left {
            padding = insets(10)
            listview(controller.onlineUsers) {
                maxWidth = 50.0
            }
        }

        right {
            vbox {
                padding = insets(10)
                spacing = 10.0

                listview(controller.messageHist)

                hbox {
                    val from = textfield("from")
                    val to = textfield("to")
                    val input = textarea {
                        prefRowCount = 2
                    }

                    button("login") {
                        action {
                            controller.login(from.text.toLong())
                        }
                    }

                    button("Send") {
                        action {
                            controller.talkToPeer(to.text.toLong(), input.text)
                        }
                    }
                    alignment = Pos.BOTTOM_CENTER
                }
            }
        }
    }
}


class MyController : Controller() {
    val client = Client("127.0.0.1", 8888)
    val messageHist: ObservableList<String> = FXCollections.observableArrayList()
    val onlineUsers: ObservableList<Long> = FXCollections.observableArrayList()
    var userId: Long = 0

    fun onReceiveMessage(message: Message): Message {
        val data = message.data
        val ack = if (data.`is`(LoginNotification::class.java)) {
            onLoginNotification(message.getLoginNotification())
        } else if (data.`is`(TalkToPeerNotification::class.java)) {
            onTalkToPeerNotification(message.getTalkToPeerNotification())
        } else if (data.`is`(LogoffNotification::class.java)) {
            onLogoffNotification(message.getLogoffNotification())
        } else {
            throw RuntimeException()
        }

        return ack.toMessage()
    }

    fun onLoginNotification(loginNotification: LoginNotification): Ack {
        launch(JavaFx) {
            onlineUsers.add(loginNotification.userId)
        }
        return Ack.newBuilder().setSuccess(true).build()
    }

    fun onTalkToPeerNotification(talkToPeerNotification: TalkToPeerNotification): Ack {
        addMessageHistory("${talkToPeerNotification.fromId}: ${talkToPeerNotification.message}")
        return Ack.newBuilder().setSuccess(true).build()
    }

    fun onLogoffNotification(logoffNotification: LogoffNotification): Ack {
        launch(JavaFx) {
            onlineUsers.remove(logoffNotification.userId)
        }
        return Ack.newBuilder().setSuccess(true).build()
    }

    fun login(userId: Long) = launch(Unconfined) {
        this@MyController.userId = userId

        client.connect {
            onReceiveMessage(Message.parseFrom(it)).toByteArray()
        }

        client.login(Login.newBuilder().setVisitorId(userId).build())
    }

    fun talkToPeer(peerId: Long, msg: String) = launch(Unconfined) {
        val v = TalkToPeer.newBuilder()
                .setPeerId(peerId)
                .setVisitorId(userId)
                .setMessage(msg)
                .build()
        client.talkToPeer(v)

        addMessageHistory("${userId}: ${v.message}")
    }

    fun addMessageHistory(msg: String) {
        launch(JavaFx) {
            messageHist.add(msg)
        }
    }
}
