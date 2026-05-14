(ns test-data)

(def test-symbol :load-file-success)

(def test-data {:message "Hello from load-file!"
                :number 42
                :vector [1 2 3]})

(defn test-function []
  "This function was loaded via load-file")
