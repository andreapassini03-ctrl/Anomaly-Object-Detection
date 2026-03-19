# Importa le librerie necessarie
import os
import numpy as np
import tensorflow as tf
from PIL import Image
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Conv2D, MaxPooling2D, UpSampling2D, Input
from sklearn.metrics import mean_squared_error

# 1. Preparazione dei Dati (caricamento e pre-elaborazione)
# funzione per caricare e preparare i dati dal dataset MVTec.
def prepare_data(data_dir, img_size=(128, 128)):
    images = []
    # Itera su tutti i file nella directory specificata
    for filename in os.listdir(data_dir):
        if filename.endswith(".png"):
            filepath = os.path.join(data_dir, filename)
            # Apri l'immagine e la converte in RGB
            img = Image.open(filepath).convert('RGB').resize(img_size)
            images.append(np.array(img))
    
    # Normalizza i dati e converte in float32
    images = np.array(images).astype('float32') / 255.0
    return images

# Utilizza solo le immagini "good" dalla cartella 'train' per l'addestramento
train_dir = 'C:/Users/andre/Downloads/mvtec_anomaly_detection/pill/train/good'
X_train_normal = prepare_data(train_dir)

print(f"Dataset di addestramento caricato con shape: {X_train_normal.shape}")

# 2. Costruzione del Modello Autoencoder
# Definisci il modello autoencoder che comprimerà e decomprimerà le immagini
def create_autoencoder(input_shape):
    input_img = Input(shape=input_shape)
    # Encoder: Comprime l'immagine
    x = Conv2D(64, (3, 3), activation='relu', padding='same')(input_img)
    x = MaxPooling2D((2, 2), padding='same')(x)
    x = Conv2D(32, (3, 3), activation='relu', padding='same')(x)
    x = MaxPooling2D((2, 2), padding='same')(x)
    x = Conv2D(16, (3, 3), activation='relu', padding='same')(x)
    encoded = MaxPooling2D((2, 2), padding='same')(x)
    # Decoder: Ricostruisce l'immagine
    x = Conv2D(16, (3, 3), activation='relu', padding='same')(encoded)
    x = UpSampling2D((2, 2))(x)
    x = Conv2D(32, (3, 3), activation='relu', padding='same')(x)
    x = UpSampling2D((2, 2))(x)
    x = Conv2D(64, (3, 3), activation='relu', padding='same')(x)
    x = UpSampling2D((2, 2))(x)
    decoded = Conv2D(input_shape[2], (3, 3), activation='sigmoid', padding='same')(x)

    autoencoder = Model(input_img, decoded)
    return autoencoder

# Imposta la forma dell'input (altezza, larghezza, canali)
input_shape = X_train_normal.shape[1:]

# Crea e compila il modello
autoencoder = create_autoencoder(input_shape)
autoencoder.compile(optimizer='adam', loss='mean_squared_error')

# 3. Addestramento del Modello
print("Inizio addestramento del modello...")
history = autoencoder.fit(X_train_normal, X_train_normal,
                          epochs=50,
                          batch_size=32,
                          shuffle=True,
                          validation_split=0.1) # Usa il 10% dei dati per la validazione
print("Addestramento completato.")

# 4. Valutazione e Soglia

# Carica i dati di test (immagini normali e anomale)
test_dir_normal = 'C:/Users/andre/Downloads/mvtec_anomaly_detection/grid/test/good'
test_dir_anomalies = 'C:/Users/andre/Downloads/mvtec_anomaly_detection/grid/test/broken' 
X_test_normal = prepare_data(test_dir_normal)
X_test_anomalies = prepare_data(test_dir_anomalies)

# Previsione sulle immagini di test
reconstructed_normal = autoencoder.predict(X_test_normal)
reconstructed_anomalies = autoencoder.predict(X_test_anomalies)

# Calcola l'errore di ricostruzione (MSE)
mse_normal = np.mean(np.power(X_test_normal - reconstructed_normal, 2), axis=(1, 2, 3))
mse_anomalies = np.mean(np.power(X_test_anomalies - reconstructed_anomalies, 2), axis=(1, 2, 3))

print(f"Errore MSE medio per immagini normali: {np.mean(mse_normal):.4f}")
print(f"Errore MSE medio per immagini anomale: {np.mean(mse_anomalies):.4f}")

# Imposta una soglia per rilevare le anomalie (es. 2 deviazioni standard sopra la media)
threshold = np.mean(mse_normal) + 2 * np.std(mse_normal)
print(f"Soglia di anomalia MSE: {threshold:.4f}")

# 5. Conversione in .tflite
print("Conversione del modello in formato TFLite...")
converter = tf.lite.TFLiteConverter.from_keras_model(autoencoder)

# Abilita le operazioni di TensorFlow non native
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS, # Operazioni native di TFLite
    tf.lite.OpsSet.SELECT_TF_OPS # Operazioni di TensorFlow Select
]
tflite_model = converter.convert()

# Salva il file .tflite
with open('anomaly_detector.tflite', 'wb') as f:
    f.write(tflite_model)
    
print("Modello convertito e salvato come anomaly_detector.tflite")

