(ns mgit.mgit
  (:require
   [babashka.fs :as fs]
   [clojure.string :as str])
  (:gen-class))

(declare actions)

(defn eprintln [& args]
  (binding [*out* *err*]
    (apply println args)))

;; (reduce f val coll)
;; f: 2つの引数を受け取る関数
;; val: 初期値
;; coll: collection
;; fにはvalとcollの最初の要素が渡される
;; (reduce + 0 [1 2 3 4]) -> (+ 0 1) -> (+ 1 2) -> (+ 3 3) -> (+ 6 4)
(defn parse
  "Parse cli option"
  [handler args]
  (reduce
   (fn [acc _]
     (let [arg (-> acc :remaining first) ; accのremainingを取り出し、そのうちの最初の要素を設定する
           more (-> acc :remaining rest) ; accのremainingを取り出し、のこりの要素を設定する
           ; ex. {:options {:args []} :remaining ["file1" "-N" "file2"]}
           ; -> arg = "file1"
           ; -> more = ["-N" "file2"]
           ]
       (or
        (when (nil? (:remaining acc))
          (reduced acc))
        (handler acc)
        (when (= arg "--")
          (reduced (-> acc
                       (update-in [:options :args] into more)
                       (assoc :remaining nil))))
        (when (-> (or arg "") (str/starts-with? "-"))
          (reduced {:error (str "Illegal argument: " arg)}))
        (-> acc
            (update-in [:options :args] conj arg)
            (update :remaining next)
            ; ex. {:options {:args []} :remaining ["file1" "-N" "file2"]}
            ; -> {:options {:args ["file1"]} :remaining ["file1" "-N" "file2"]}
            ; -> {:options {:args ["file1"]} :remaining ["-N" "file2"]}
            ))))
   {:options {:args []} :remaining args}
   ;; accumurator内では利用されない。あくまでループの回数に利用される
   args))

(def git-dir (fs/file ".git"))
(def objects-dir (fs/file git-dir "objects"))
(def refs-dir (fs/file git-dir "refs"))

(defn cmd-init
  "Initialize git repository"
  [& _args]
  ;; 1. create directories
  (doseq [dir [git-dir objects-dir refs-dir]]
    (-> dir fs/create-dirs))
  ;; 2. write content to ${git-dir}/HEAD
  (->> "ref: refs/heads/main\n"
       (spit (fs/file git-dir "HEAD")))
  ;; 3. report to finish
  (-> (format "Initialized empty Git repository in %s" (fs/absolutize git-dir))
      eprintln))

(defn cmd-add
  "Add file contents to the index"
  [& args]
  (let [parsed (->> args
                    (parse
                     (fn [acc]
                       (condp apply [(-> acc :remaining first)]
                         #{"-N"} (-> acc
                                     (assoc-in [:options :N] true)
                                     (update :remaining next))
                         nil))))]
    (eprintln parsed)))

(defn cmd-help
  "Help"
  [& _args]
  (println "Available actions:")
  (doseq [[k v] actions]
    (-> (format " %s - %s" k (:doc (meta v)))
        eprintln)))

(def actions {"init" #'cmd-init
              "add" #'cmd-add
              "help" #'cmd-help})

(defn -main [& args]
  (let [action (get actions (first args))]
    (if action
      (apply action args)
      (do
        (when (first args)
          (println (format "`%s` is not a mgit action.\n" (first args))))
        (cmd-help args)))))
