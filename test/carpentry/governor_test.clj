(ns carpentry.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [carpentry.store :as store]
            [carpentry.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Carpentry"})
    (store/register-job! st {:job-id "J-1" :client-id "client-1"
                             :name "deck-build"
                             :material-stock {"2x4-pine" 40 "plywood-sheet" 10}})
    st))

(defn- cut [material qty]
  {:op :approve-cut :effect :propose :job-id "J-1"
   :material material :quantity qty :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-stock
  (let [st (fresh-store)
        v (governor/check req {} (cut "2x4-pine" 20) st)]
    (is (:ok? v))))

(deftest ok-at-exact-stock
  (testing "cutting exactly the on-hand quantity is within margin"
    (let [st (fresh-store)
          v (governor/check req {} (cut "plywood-sheet" 10) st)]
      (is (:ok? v)))))

(deftest hard-on-stock-exceeded
  (testing "you cannot cut what you don't have"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (cut "2x4-pine" 60) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :stock-exceeded (:rule %)) (:violations v))))))

(deftest hard-on-unknown-material
  (let [st (fresh-store)
        v (governor/check req {} (cut "steel-beam" 5) st)]
    (is (:hard? v))
    (is (some #(= :unknown-material (:rule %)) (:violations v)))))

(deftest hard-on-unknown-job
  (let [st (fresh-store)
        v (governor/check req {} (assoc (cut "2x4-pine" 20) :job-id "J-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-job (:rule %)) (:violations v)))))

(deftest hard-on-foreign-job
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (cut "2x4-pine" 20) st)]
      (is (:hard? v))
      (is (some #(= :job-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (cut "2x4-pine" 20) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (cut "2x4-pine" 20) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-powered-saw-operation-even-at-high-confidence
  (testing "no robot operation near powered saws without the governor gate"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-powered-saw-operation :effect :propose
                                    :job-id "J-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-height-work-even-at-high-confidence
  (testing "working-at-height tasks require human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-height-work :effect :propose
                                    :job-id "J-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (cut "2x4-pine" 20) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
