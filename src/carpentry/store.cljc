(ns carpentry.store
  "SSoT for the ISCO-08 7115 independent carpentry practice actor
  (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors section;
  README's 'Robotics premise' — a cutting-support and
  material-handling robot performs precision measurement and panel
  positioning under this advisor/governor pair, which never
  dispatches hardware itself). Modeled on cloud-itonami-isco-4311's
  bookkeeping.store.

  Domain:

    client — a registered organization (:client-id, :name)
    job    — a registered job {:job-id :client-id :name
             :material-stock {material-str number}}.
             `:material-stock` is the registered on-hand quantity per
             material a proposed cut's material and quantity must be
             validated against — you cannot cut a material that isn't
             on the job's registered stock list, and you cannot cut
             more of it than is registered on hand.
    record — a committed operating record (approved cut) — written
             ONLY via commit-record!.
    ledger — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (job [s job-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-job! [s j])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (job [_ job-id] (get-in @a [:jobs job-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-job! [s j]
    (swap! a assoc-in [:jobs (:job-id j)] j) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :jobs {} :records [] :ledger []}
                                   seed)))))
