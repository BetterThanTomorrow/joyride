(ns require-js-file
  (:require ["../src/hello-world.js" :as hello-world]
            ;; You can use absolute paths as well.
            #_["/Users/pez/Projects/joyride/examples/.joyride/src/hello-world.js" :as absolute-hw]))

;; Run this as a Workspace Script or load it in the REPL.

(comment
  (hello-world/hello)
  #_(absolute-hw/hello)
  :rcf)

(hello-world/showHelloMessage)

;; Then update `../src/hello-world.js` and run the script again.
;; Or, if you are connected to the REPL, reload the file
;;   or evaluate the `ns` form, and then evalute the hello forms