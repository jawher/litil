let a = 1
let b = 2

let ax x = a * x

let a = 4

let res = ax 2


let g x =
  let z = 4
  let res = \x -> z * x
  let z = 0
  res x

let res = g 2

let g x =
  let z = 4
  let h x = z * x
  let z = 0
  h x


let res = g 2

let res = (\x->\y->x+y) 5 3
