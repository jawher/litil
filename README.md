Litil
======

This is the first post of a series where I'll talk about the different techniques I've used to create a parser and un evaluator for the Litil™  programming language in Java.
However, please keep the following in mind:

This is still *work in progress*, this a a bare-bones languages: no REPL, no tools, no language spec, nothing but code, and even that isn't that pretty to look at not particularly clean or organized.

Here's a quick overview of the Litil language


* A functional language heavily inspired by ML, (O)Caml, Haskell and [Roy](http://roy.brianmckenna.org/)
* Full [Hindley Milner](http://en.wikipedia.org/wiki/Hindley-Milner_type_inference) type inference
* [Python like use of indentation to define blocs](http://en.wikipedia.org/wiki/Off-side_rule)
* Supported types: numerics, strings, characters and boolean, [tuples](http://en.wikipedia.org/wiki/Tuple), [records](http://en.wikipedia.org/wiki/Record_\(computer_science\)) and [ADTs](http://en.wikipedia.org/wiki/Algebraic_data_type)
* [pattern matching](http://en.wikipedia.org/wiki/Pattern_matching)
* [curried functions](http://en.wikipedia.org/wiki/Currying) by default
* [closures](http://en.wikipedia.org/wiki/Closure_\(computer_science\))
* exceptions (try/catch/throw)

Now, some teaser examples to give you a feel for the language:

## assignment, expressions & functions

```litil
let fact n =
  if n <= 2 then
    2
  else
    n * fact (n-1)

let f5 = fact 5
```


## tuples & records

```litil
let x = (5, "a")
let person = {name: "lpe", age: 12}
```

## destructuring

```litil
let (a, b) = (4, "d")

let d = ((4, true), ("test", 'c', a))

let ((_, bool), (_, _, _)) = d
```


## algebraic data types

```litil
data Option a = Some a | None

let o = Some "thing"

data List a = Cons a (List a) | Nil

let l = Cons 5 (Cons 6 Nil)

data Tree a = Null | Leaf a | Node (Tree a) a (Tree a)

let t = Node (Leaf 5) 4 (Leaf 3)
```

## pattern matching

```litil
let len l =
  match l
    []     => 0
    _ :: t => 1 + len t

len [1, 2, 3]
```

## partial application

```litil
let add x y = x + y

let inc = add 1

let three = inc 2
```

## closures & higher-order functions

```litil
let map f xs =
  match xs
    []     => Nil
    h :: t => (f h) :: (map f t)

let l = [1, 2]

let double x = 2 * x

-- pass a function by name
let l2 = map double l

-- or simply a lambda
let l2 = map (\x => 2 * x) l

let a = 4
let f = \x => a * x -- f captures the lexical value of a, i.e. 4
let a = 5
f 5
```

License
-------

See `LICENSE` for details (hint: it's MIT).
