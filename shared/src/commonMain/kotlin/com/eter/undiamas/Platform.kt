package com.eter.undiamas

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform