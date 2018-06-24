package com.jockeyjs

 fun Jockey.on(on: String, function: (payload: Map<Any, Any>?) -> Unit) {
    this.on(on, object : JockeyHandler() {
        override fun doPerform(payload: Map<Any, Any>?) {
            function(payload)
        }

    })
}