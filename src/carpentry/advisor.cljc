(ns carpentry.advisor
  "CarpentryAdvisor — the advisor named in this repository's README,
  proposing a job operation (approve a cut, approve powered-saw
  operation, approve height work) from a job order, material list and
  safety plan. Swappable mock/llm; the advisor ONLY proposes —
  `carpentry.governor` checks the material basis and stock ceiling
  independently and always escalates powered-saw/height-work
  decisions. Modeled on cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-cut|:approve-powered-saw-operation|:approve-height-work
               :effect :propose :job-id str :material str
               :quantity number :stake kw :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake job-id material quantity] :as request}]
  {:op op
   :effect :propose
   :job-id job-id
   :material material
   :quantity quantity
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a carpentry-practice advisor. Given a request, propose an
   :op, the :job-id, :material and :quantity, an honest :confidence
   and a :stake. Never call an unregistered-material or over-stock cut
   conforming — the governor checks both against the registered job
   record. Powered-saw and height-work decisions always require human
   sign-off regardless of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
