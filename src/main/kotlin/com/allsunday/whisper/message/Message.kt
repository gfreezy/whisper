package com.allsunday.whisper.message

data class Login(val id: Login, val name: String, val password: String)

data class Message(val id: Long, val from: String, val to: String, val text: String)

data class Ack(val ackTo: Long)
