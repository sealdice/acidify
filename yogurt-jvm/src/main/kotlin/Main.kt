@file:JvmName("JvmMain")

import java.io.PrintStream

fun main(args: Array<String>) {
    val utf8out = PrintStream(System.out, true, "UTF-8")
    val utf8err = PrintStream(System.err, true, "UTF-8")
    System.setOut(utf8out)
    System.setErr(utf8err)
    org.ntqqrev.yogurt.main(args)
}
