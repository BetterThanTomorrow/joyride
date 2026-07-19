(ns editscript-hello
  (:require [editscript.core :as e]))

;; Here are two pieces of data, a and b
(def a ["Hello word" 24 22 {:a [1 2 3]} 1 3 #{1 2}])
(def b ["Hello world" 24 23 {:a [2 3]} 1 3 #{1 2 3}])

;; compute the editscript between a and b using the default options
(def d (e/diff a b))

;; look at the editscript
(e/get-edits d)
;;==>
;; [[[0] :r "Hello world"] [[2] :r 23] [[3 :a 0] :-] [[6 3] :+ 3]]

;; diff using the quick algorithm and diff the strings by character
;; there are other string diff levels: :word, :line, or :none (default)
(def d-q (e/diff a b {:algo :quick :str-diff :character}))

(e/get-edits d-q)
;;=>
;; [[[0] :s [9 [:+ "l"] 1]] [[2] :r 23] [[3 :a 0] :-] [[6 3] :+ 3]]

;; get the edit distance, i.e. number of edits
(e/edit-distance d)
;;==> 4

;; get the size of the editscript, i.e. number of nodes
(e/get-size d)
;;==> 23

;; patch a with the editscript to get back b, so that
(= b (e/patch a d))
;;==> true
(= b (e/patch a d-q))
;;==> true