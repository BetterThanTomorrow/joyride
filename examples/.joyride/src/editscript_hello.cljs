(ns editscript-hello
  (:require [joyride.core :as joyride]))

(def src-base "https://raw.githubusercontent.com/juji-io/editscript/b493ccf2e987fed84a05badb2626eebae4b91328/src/editscript/")

(def editscript-files
  ["edit.cljc"
   "util/pairing.cljc"
   "util/common.cljc"
   "util/index.cljc"
   "patch.cljc"
   "diff/quick.cljc"
   "diff/a_star.cljc"
   "core.cljc"])

(defn ^:async load-editscript!+ []
  (if (some-> (find-ns 'editscript.core) ns-publics (get 'diff))
    :already-loaded
    (do
      (doseq [f editscript-files]
        (let [src (await (joyride/slurp (str src-base f)))]
          (load-string src)))
      (def diff (ns-resolve 'editscript.core 'diff))
      (def get-edits (ns-resolve 'editscript.core 'get-edits))
      (def edit-distance (ns-resolve 'editscript.core 'edit-distance))
      (def get-size (ns-resolve 'editscript.core 'get-size))
      (def patch (ns-resolve 'editscript.core 'patch))
      :loaded)))

(comment
  ;; Slurp editscript sources from GitHub
  (load-editscript!+)
  
  ;; From: https://github.com/juji-io/editscript#tada-usage

  ;; Here are two pieces of data, a and b
  (def a ["Hello word" 24 22 {:a [1 2 3]} 1 3 #{1 2}])
  (def b ["Hello world" 24 23 {:a [2 3]} 1 3 #{1 2 3}])
  
  ;; compute the editscript between a and b using the default options
  (def d (diff a b))

  ;; look at the editscript
  (get-edits d)
  ;;=> [[[0] :r "Hello world"] [[2] :r 23] [[3 :a 0] :-] [[6 3] :+ 3]]
  
  ;; diff using the quick algorithm and diff the strings by character
  ;; there are other string diff levels: :word, :line, or :none (default)
  (def d-q (diff a b {:algo :quick :str-diff :character}))

  (get-edits d-q)
  ;;=> [[[0] :s [9 [:+ "l"] 1]] [[2] :r 23] [[3 :a 0] :-] [[6 3] :+ 3]]
  
  ;; get the edit distance, i.e. number of edits
  (edit-distance d)
  ;;=> 4
  
  ;; get the size of the editscript, i.e. number of nodes
  (get-size d)
  ;;=> 23
  
  ;; patch a with the editscript to get back b, so that
  (= b (patch a d))
  ;;=> true
  (= b (patch a d-q))
  ;;=> true
  
  :rcf)

