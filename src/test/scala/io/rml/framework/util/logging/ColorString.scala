package io.rml.framework.util.logging

/**
  * Credits to:
  * https://github.com/ktoso/scala-rainbow
  */
trait ColorString {
  implicit def color(s: String) = new ColoredString(s)

  class ColoredString(s: String) {

    import Console._

    /** Colorize the given string foreground to ANSI red */
    def red = RED + s + RESET

    /** Colorize the given string foreground to ANSI green */
    def green = GREEN + s + RESET

    /** Colorize the given string foreground to ANSI yellow */
    def yellow = YELLOW + s + RESET

    /** Colorize the given string foreground to ANSI blue */
    def blue = BLUE + s + RESET

  }

}