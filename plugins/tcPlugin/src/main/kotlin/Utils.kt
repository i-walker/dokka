package org.jetbrains.dokka.tc

import arrow.Kind
import arrow.core.ListK
import arrow.core.Nel
import arrow.core.Validated
import arrow.core.ValidatedNel
import arrow.core.combine
import arrow.core.extensions.list.foldable.reduceLeftOption
import arrow.core.extensions.listk.semigroup.semigroup
import arrow.core.extensions.nonemptylist.semigroup.semigroup
import arrow.core.getOrElse
import arrow.core.validNel
import arrow.fx.typeclasses.Async

const val ANSI_RESET = "\u001B[0m"
const val ANSI_GREEN = "\u001B[32m"
const val ANSI_RED = "\u001B[31m"

fun colored(color: String, message: String) =
  "$color$message$ANSI_RESET"

fun <A, EE, F> List<Kind<F, ValidatedNel<EE, A>>>.collectErrors(AF: Async<F>): Kind<F, Kind<F, ValidatedNel<EE, List<A>>>> =
  AF.run {
    effect {
      map { fa ->
        fa.map { validated: ValidatedNel<EE, A> ->
          validated.map { a: A -> ListK.just(a) }
        }
      }.reduceLeftOption { fa, fb ->
        fa.map2(fb) { (a: Validated<Nel<EE>, ListK<A>>, b: Validated<Nel<EE>, ListK<A>>) ->
          a.combine(Nel.semigroup(), ListK.semigroup(), b)
        }
      }.getOrElse { just(ListK.empty<A>().validNel()) }
    }
  }
