(ns gym.envs.cartpole
  (:require
    [engine.gameloop :as gameloop]
    [engine.physics :refer :all]))

(def MAX-ANGLE 0.125)

(def SHIP-SPEED 7.5)

(defn spawn [state]
  (create-body:rectangle [[0 104] [4 4]] :tip)
  (create-body:rectangle [[0 0] [4 200]] :pole)
  (create-joint:weld :tip-rod :pole :tip [0.0 102.0])
  (translate-bodies [:pole :tip] [200 -194])

  (create-body:rectangle [[-150.0 -50] [25 100]] :pillar-left :infinite)
  (create-body:rectangle [[150.0 -50] [25 100]] :pillar-right :infinite)
  (create-body:rectangle [[-156.25 5] [12.5 10]] :pillar-leftmost :infinite)
  (create-body:rectangle [[156.50 5] [12.5 10]] :pillar-rightmost :infinite)
  (translate-bodies
   [:pillar-leftmost :pillar-left :pillar-right :pillar-rightmost] [200 -300])

  (create-body:rectangle [[0 0] [300 2.0]] :rail)

  (create-body:rectangle [[0 0] [30 10]] :cart)
  (create-body:rectangle [[-6 6] [4 4]] :constraint-left)
  (create-body:rectangle [[6 6] [4 4]] :constraint-right)

  (create-joint:weld :weld1 :cart :constraint-left [-6.0 6.0])
  (create-joint:weld :weld2 :cart :constraint-right [6.0 6.0])

  (create-joint:prismatic :prismatic :rail :cart [-50.0 0.0] [50.0 0.0])
  (translate-bodies [:rail] [200 -299])
  (translate-bodies [:cart :constraint-left :constraint-right] [200 -304])

  state)

(defn reset [state]
  (remove-bodies-and-joints)
  (spawn state))

(defn handle-control [state keys-pressed cmd notify]
  (do
    (cond
      (or (.contains keys-pressed \a) (= cmd :left))
      (set-motor-speed :prismatic SHIP-SPEED)
      (or (.contains keys-pressed \d) (= cmd :right))
      (set-motor-speed :prismatic (- SHIP-SPEED))
      :else (set-motor-speed :prismatic 0)
      )
    (let [state (-> state
                    (assoc-in [:observation :pole-rotation] (get-rotation :pole))
                    (assoc-in [:observation :tip-velocity] (get-linear-velocity :tip))
                    (assoc-in [:observation :cart-position] (- (first (get-position :cart)) 200.0))
                    (assoc-in [:observation :cart-velocity] (get-linear-velocity :cart)))]
      (-> state
          (cond-> (or (> (-> state :observation :pole-rotation) MAX-ANGLE)
                      (< (-> state :observation :pole-rotation) (- MAX-ANGLE))) (assoc-in [:observation :done] true))
          (notify)))))

;; :cmd
;;   :end   - ends processing
;;   :reset - resets the environment
;;   :left  - moves the ship to the left
;;   :right - moves the ship to the right
;; :observation
;;   :done     - when the rod reached a specified maximum angle
;;   :cart-position
;;   :pole-rotation
;;   :cart-velocity - x component of the tip's velocity
;;   :tip-velocity - x component of the tip's velocity
(defn on-tick-observable [notify notify-end]
  (fn on-tick [{cmd :cmd finished :finished :as state} keys-pressed tick-in-ms]
    (if finished
      state
      (cond
        (= :reset cmd)
        (do (reset state)
          (-> state
              (assoc :cmd nil)
              (assoc-in [:observation :done] false)
              (notify)))
        (= :end cmd)
        (do
          (-> state
              (assoc :finished true)
              (notify-end)))
        :else
        (handle-control state keys-pressed cmd notify)))))

(defn go [on-tick-observer on-end-observer initial-state]
  (gameloop/start (spawn initial-state) (on-tick-observable on-tick-observer on-end-observer)))

(defn -main [& args]
  (go (fn [{{done :done} :observation :as state}]
        (if done
          (assoc state :cmd :reset)
          state)
        ) identity {}))