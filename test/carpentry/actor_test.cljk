(ns carpentry.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [carpentry.actor :as actor]
            [carpentry.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Carpentry"})
    (store/register-job! st {:job-id "J-1" :client-id "client-1"
                             :name "deck-build"
                             :material-stock {"2x4-pine" 40}})
    st))

(deftest commits-an-in-stock-cut
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-cut :stake :low
                 :job-id "J-1" :material "2x4-pine" :quantity 20}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-over-stock-cut
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-cut :stake :low
                 :job-id "J-1" :material "2x4-pine" :quantity 90}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-approves-height-work-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-height-work :stake :low
                 :job-id "J-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
