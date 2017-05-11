package com.allsunday.whisper

import com.allsunday.whisper.proto.*
import com.google.protobuf.Any


fun Message.getLogin(): Login {
    return data.unpack(Login::class.java)
}

fun Message.getAck(): Ack {
    return data.unpack(Ack::class.java)
}

fun Message.getTalkToPeer(): TalkToPeer {
    return data.unpack(TalkToPeer::class.java)
}

fun Message.getLoginNotification(): LoginNotification {
    return data.unpack(LoginNotification::class.java)
}

fun Message.getTalkToPeerNotification(): TalkToPeerNotification {
    return data.unpack(TalkToPeerNotification::class.java)
}

fun Message.getLogoff(): Logoff {
    return data.unpack(Logoff::class.java)
}


fun Message.getLogoffNotification(): LogoffNotification {
    return data.unpack(LogoffNotification::class.java)
}


fun Login.toMessage(): Message {
    return Message.newBuilder().setData(Any.pack(this)).build()
}

fun TalkToPeer.toMessage(): Message {
    return Message.newBuilder().setData(Any.pack(this)).build()
}

fun Ack.toMessage(): Message {
    return Message.newBuilder().setData(Any.pack(this)).build()
}

fun TalkToPeerNotification.toMessage(): Message {
    return Message.newBuilder().setData(Any.pack(this)).build()
}

fun LoginNotification.toMessage(): Message {
    return Message.newBuilder().setData(Any.pack(this)).build()
}

fun Logoff.toMessage(): Message {
    return Message.newBuilder().setData(Any.pack(this)).build()
}

fun LogoffNotification.toMessage(): Message {
    return Message.newBuilder().setData(Any.pack(this)).build()
}
