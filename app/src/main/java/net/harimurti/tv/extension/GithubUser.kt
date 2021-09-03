package net.harimurti.tv.extension

import net.harimurti.tv.model.GithubUser

fun Array<GithubUser>?.toStringContributor(): String? {
    if (this == null || this.isEmpty()) return null
    val users = StringBuilder()
    for (i in this.indices) {
        users.append(this[i].login)
        if (i < this.size - 1) users.append(", ")
    }
    return users.toString()
}