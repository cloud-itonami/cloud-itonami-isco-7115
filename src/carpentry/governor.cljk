(ns carpentry.governor
  "CarpentryGovernor — the independent safety/traceability layer
  named in this repository's README/business-model.md, gating the
  robot-dispensed physical work (precision measurement, panel
  positioning) an advisor may propose. The governor never dispatches
  hardware itself. Modeled on cloud-itonami-isco-4311's
  bookkeeping.governor. Material twist: a proposed cut's material must
  be a REGISTERED key in the job's material stock (no fabricated
  material), and the proposed quantity is arithmetic comparison
  against the registered on-hand stock for that material — you cannot
  cut what you don't have.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose (the
                           governor never dispatches hardware; it only
                           gates what the robot may execute).
    3. job basis            — a cut approval must cite a REGISTERED
                           job belonging to this client.
    4. material basis       — the proposed material must be a
                           REGISTERED key in the job's
                           :material-stock map (no fabricated
                           material).
    5. stock ceiling        — the proposed quantity must not exceed
                           the registered on-hand stock for that
                           material (you cannot cut what you don't
                           have).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :approve-powered-saw-operation (no robot operation near
                           powered saws without the governor gate).
    7. :op :approve-height-work (working-at-height tasks require
                           human sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [carpentry.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-powered-saw-operation
                                     :approve-height-work})

(defn- hard-violations [{:keys [request proposal]} client-record j]
  (let [{:keys [op material quantity]} proposal
        cut? (= :approve-cut op)
        stock (:material-stock j)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はハードウェアを直接起動しない）"})

      (and cut? (nil? j))
      (conj {:rule :unknown-job :detail "未登録 job への切断承認は不可"})

      (and cut? j (not= (:client-id j) (:client-id request)))
      (conj {:rule :job-wrong-client :detail "job が別 client のもの"})

      (and cut? j material (not (contains? stock material)))
      (conj {:rule :unknown-material :detail (str "未登録材料: " material "（材料の捏造禁止）")})

      (and cut? j material (contains? stock material) (number? quantity)
           (> quantity (get stock material)))
      (conj {:rule :stock-exceeded
             :detail (str "切断数量 " quantity " > 在庫 " (get stock material)
                          "（存在しない材料は切断できない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `carpentry.store/Store`. Pure — never mutates
  the store, never dispatches the robot."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        j (some->> (:job-id proposal) (store/job store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record j)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
