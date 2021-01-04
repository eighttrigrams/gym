(ns engine.physics
  (:import (org.dyn4j.dynamics Body World)
           (org.dyn4j.dynamics.joint Joint MotorJoint RevoluteJoint WeldJoint PrismaticJoint)
           (org.dyn4j.geometry Rectangle MassType Vector2 Mass)))

(defonce world (World.))

(defonce joints (atom {}))

(defonce bodies (atom {}))

(defn create-body:rectangle
  ([t id]
   (create-body:rectangle t id :normal))
  ([[[x-pos y-pos] [width height] & [rotation]] id mass-type]
   (let [body      (Body.)
         rectShape (Rectangle. width height)]
     (.addFixture body rectShape)
     (.setUserData body {:width width :height height :id id :type :rectangle})
     (.setMass body (if (= mass-type :infinite) MassType/INFINITE MassType/NORMAL))
     (when (not (nil? rotation)) (.rotate body rotation))
     (.translate body x-pos y-pos)
     (.addBody world body)
     (swap! bodies assoc id body))))

(defn get-engine-bodies
  ([] (get-engine-bodies nil))
  ([keys]
   (if (not bodies)
     '()
     (map
      (fn prepare-body
        [^Body body]
        (let [user-data (.getUserData body)]
          {:x          (.getTranslationX (.getTransform body))
           :y          (.getTranslationY (.getTransform body))
           :properties user-data
           :body       body
           :rotation   (- (.getRotationAngle (.getTransform body)))}))
      (vals @bodies)))))

(defn step-in-ms [tick-in-ms]
  (.step world tick-in-ms))

(defn create-joint:revolute [id body1-id body2-id [x y]]
  (let [joint (RevoluteJoint. (body1-id @bodies)
                              (body2-id @bodies)
                              (Vector2. x y))]
    (.setReferenceAngle joint 0.0)
    (.setLimitEnabled joint true)
    (.setLowerLimit joint -1.5)
    (.setUpperLimit joint 0.1)
    (.addJoint world joint)
    (.setMotorEnabled joint true)
    (.setMotorSpeed joint 0.05)
    (swap! joints assoc id joint)))

(defn create-joint:weld [id body1-id body2-id [x y]]
  (let [joint (WeldJoint. (body1-id @bodies)
                          (body2-id @bodies)
                          (Vector2. x y))]
    (.setCollisionAllowed joint false)
    (.addJoint world joint)
    (swap! joints assoc id joint)))

(defn create-joint:prismatic [id body1-id body2-id [x1 y1] [x2 y2]]
  (let [joint (PrismaticJoint. (body1-id @bodies)
                               (body2-id @bodies)
                               (Vector2. x1 y1)
                               (Vector2. x2 y2))]
    (.setCollisionAllowed joint false)
    (.setMotorEnabled joint true)
    (.setMaximumMotorForce joint 12000.0)
    (.addJoint world joint)
    (swap! joints assoc id joint)))

(defn remove-bodies-and-joints []
  (mapv #(.removeJoint world %) (vals @joints))
  (reset! joints {})
  (mapv #(.removeBody world %) (vals @bodies))
  (reset! bodies {}))

(defn get-rotation [id]
  (.getRotationAngle (.getTransform (id @bodies))))

(defn get-linear-velocity [id]
  (.x (.getLinearVelocity (id @bodies))))

(defn set-motor-speed [joint-id speed]
  (.setMotorSpeed (joint-id @joints) speed))

(defn set-maximum-motor-torque [joint-id max-torque]
  (.setMaximumMotorTorque (joint-id @joints) max-torque))

(defn translate-bodies [ids [x y]]
  (mapv #(.translate (% @bodies) x y) ids))

(defn set-linear-velocity [id [x y]]
  (.setLinearVelocity (id @bodies) x y))
