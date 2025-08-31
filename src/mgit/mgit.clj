(ns mgit.mgit
  (:require [babashka.fs :as fs])
  (:gen-class))

(declare actions)

(defn eprintln [& args]
  (binding [*out* *err*]
    (apply println args)))

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

(defn cmd-help
  "Help"
  [& _args]
  (println "Available actions:")
  (doseq [[k v] actions]
    (-> (format " %s - %s" k (:doc (meta v)))
        eprintln)))

(def actions {"init" #'cmd-init
              "help" #'cmd-help})

(defn -main [& args]
  (let [action (get actions (first args))]
    (if action
      (apply action args)
      (do
        (when (first args)
          (println (format "`%s` is not a mgit action.\n" (first args))))
        (cmd-help args)))))
