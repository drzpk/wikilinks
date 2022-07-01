package dev.drzepka.wikilinks.updater

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler

@Suppress("unused")
class LambdaHandler : RequestHandler<Any, Any> {

    override fun handleRequest(input: Any?, context: Context): Any? {
        main()
        return null
    }
}
