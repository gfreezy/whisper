package com.allsunday.whisper.actor

import kotlinx.coroutines.experimental.channels.ActorJob
import java.util.concurrent.ConcurrentHashMap

class RegisteredActor(val name: String) : Exception()

class ActorNotRegistered(val name: String) : Exception()


object ActorRegistry {
    val registry: ConcurrentHashMap<String, ActorJob<Any>> = ConcurrentHashMap()

    fun register(name: String, actorRef: ActorJob<Any>) {
        if (registry.contains(name)) {
            throw RegisteredActor(name)
        }
        registry.put(name, actorRef)
    }

    fun deregister(name: String) {
        if (!registry.contains(name)) {
            throw ActorNotRegistered(name)
        }
        registry.remove(name)
    }

    fun getActor(name: String): ActorJob<Any>? {
        return registry[name]
    }
}
