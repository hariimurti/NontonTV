package net.harimurti.tv.extra

import net.harimurti.tv.R
import net.harimurti.tv.App
import net.harimurti.tv.model.GithubUser

fun Array<GithubUser>?.toStringContributor(): String? {
    if (this == null || this.isEmpty()) return null
    val users = StringBuilder()
    for (user in this) {
        users.append(user.login).append(", ")
    }
    val support = App.context.getString(R.string.thanks_for_support)
    return users.append(support).toString()
}