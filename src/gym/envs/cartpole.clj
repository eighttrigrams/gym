(ns gym.envs.cartpole
  (:require
    [engine.gameloop :as gameloop]
    [engine.physics :refer :all]))

(def MAX-ANGLE 0.25)

(def SHIP-SPEED 10)

(defn spawn []
  (create-body:rectangle [[0 104] [4 4]] :tip)
  (create-body:rectangle [[0 0] [4 200]] :rod)
  (create-joint:weld :tip-rod :rod :tip [0.0 102.0])
  (translate-bodies [:rod :tip] [200 -194])
  (set-linear-velocity :tip [30.0 0.0])

  (create-body:rectangle [[-150.0 -50] [25 100]] :pillar-left :infinite)
  (create-body:rectangle [[150.0 -50] [25 100]] :pillar-right :infinite)
  (create-body:rectangle [[-156.25 5] [12.5 10]] :pillar-leftmost :infinite)
  (create-body:rectangle [[156.50 5] [12.5 10]] :pillar-rightmost :infinite)
  (translate-bodies
   [:pillar-leftmost :pillar-left :pillar-right :pillar-rightmost] [200 -300])

  (create-body:rectangle [[0 0] [300 2.0]] :rail)

  (create-body:rectangle [[0 0] [30 10]] :ship)
  (create-body:rectangle [[-10 10] [10 8]] :constraint-left)
  (create-body:rectangle [[10 10] [10 8]] :constraint-right)

  (create-joint:weld :weld1 :ship :constraint-left [-10.0 10.0])
  (create-joint:weld :weld2 :ship :constraint-right [10.0 10.0])

  (create-joint:prismatic :prismatic :rail :ship [-50.0 0.0] [50.0 0.0])
  (translate-bodies [:rail] [200 -299])
  (translate-bodies [:ship :constraint-left :constraint-right] [200 -304])
  {})

(defn reset []
  (remove-bodies-and-joints)
  (spawn))

(defn on-tick-observable [notify]
  (fn on-tick [{cmd :cmd :as state} keys-pressed tick-in-ms]
    (if
      (= :reset cmd)
      (do (reset)
        (-> state
            (assoc :cmd nil)
            (assoc :done false)
            (notify)))
      (do
        (cond
          (.contains keys-pressed \a)
          (set-motor-speed :prismatic SHIP-SPEED)
          (.contains keys-pressed \d)
          (set-motor-speed :prismatic (- SHIP-SPEED))
          :else (set-motor-speed :prismatic 0))
        (let [state (-> state
                        (assoc :rotation (get-rotation :rod))
                        (assoc :velocity (get-linear-velocity :tip)))]
          (-> state
              (cond-> (or (> (:rotation state) MAX-ANGLE)
                          (< (:rotation state) (- MAX-ANGLE))) (assoc :done true))
              (notify)))))))

(defn go [on-tick-observer]
  (gameloop/start (spawn) (on-tick-observable on-tick-observer)))
