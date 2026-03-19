# Importa le librerie necessarie
import os
import numpy as np
import tensorflow as tf
from PIL import Image
from tensorflow.keras.models import Model
from tensorflow.keras.layers import Conv2D, MaxPooling2D, UpSampling2D, Input, Dropout
from tensorflow.keras.preprocessing.image import ImageDataGenerator
import matplotlib.pyplot as plt

# Quantization - API: TensorFlow Model Optimization Toolkit
converter.optimization = [tf.lite.Optimize.DEFAULT]

# Funzione per caricare e preparare i dati
def prepare_data(data_dir, img_size=(128, 128)):
    images = []
    # Verifica che la directory esista per evitare errori
    if not os.path.isdir(data_dir):
        print(f"Errore: La directory {data_dir} non esiste. Aggiorna il percorso.")
        return np.array([])
    
    for filename in os.listdir(data_dir):
        if filename.endswith(".png"):
            filepath = os.path.join(data_dir, filename)
            try:
                # Apri l'immagine, la converte in RGB e ridimensiona
                img = Image.open(filepath).convert('RGB').resize(img_size)
                images.append(np.array(img))
            except Exception as e:
                print(f"Impossibile caricare l'immagine {filename}: {e}")
                continue
    
    if not images:
        print(f"Attenzione: Nessuna immagine PNG trovata in {data_dir}.")
        return np.array([])
    
    # Normalizza i dati e converte in float32
    images = np.array(images).astype('float32') / 255.0
    return images

# Istruzioni: Aggiorna questi percorsi con i tuoi percorsi reali //prima era pill 
train_dir = 'C:/Users/andre/Downloads/mvtec_anomaly_detection/grid/train/good'
test_dir_normal = 'C:/Users/andre/Downloads/mvtec_anomaly_detection/grid/test/good'
test_dir_anomalies = 'C:/Users/andre/Downloads/mvtec_anomaly_detection/grid/test/broken'

X_train_normal = prepare_data(train_dir)
if X_train_normal.size == 0:
    print("Addestramento annullato a causa di dati mancanti.")
else:
    print(f"Dataset di addestramento caricato con shape: {X_train_normal.shape}")

    # 2. Costruzione del Modello Autoencoder Migliorato
    def create_autoencoder_improved(input_shape):
        input_img = Input(shape=input_shape)
        # Encoder: Aggiungi più strati per una migliore compressione
        x = Conv2D(64, (3, 3), activation='relu', padding='same')(input_img)
        x = MaxPooling2D((2, 2), padding='same')(x)
        x = Conv2D(128, (3, 3), activation='relu', padding='same')(x)
        x = MaxPooling2D((2, 2), padding='same')(x)
        x = Conv2D(64, (3, 3), activation='relu', padding='same')(x)
        x = MaxPooling2D((2, 2), padding='same')(x)
        x = Dropout(0.2)(x) # Dropout per regolarizzazione
        
        encoded = Conv2D(32, (3, 3), activation='relu', padding='same')(x)
        
        # Decoder: Aggiungi più strati per una migliore ricostruzione
        x = Conv2D(64, (3, 3), activation='relu', padding='same')(encoded)
        x = UpSampling2D((2, 2))(x)
        x = Conv2D(128, (3, 3), activation='relu', padding='same')(x)
        x = UpSampling2D((2, 2))(x)
        x = Conv2D(64, (3, 3), activation='relu', padding='same')(x)
        x = UpSampling2D((2, 2))(x)
        
        decoded = Conv2D(input_shape[2], (3, 3), activation='sigmoid', padding='same')(x)

        autoencoder = Model(input_img, decoded)
        return autoencoder

    input_shape = X_train_normal.shape[1:]
    autoencoder = create_autoencoder_improved(input_shape)
    autoencoder.compile(optimizer='adam', loss='mean_squared_error')
    autoencoder.summary() # Stampa un riepilogo del modello per visualizzare i cambiamenti

    # 3. Data Augmentation e Addestramento
    # Creazione del generatore di dati per l'augmentation
    # I parametri scelti sono piccoli e realistici per variazioni non anomale
    datagen = ImageDataGenerator(
        rotation_range=5,
        width_shift_range=0.05,
        height_shift_range=0.05,
        zoom_range=0.05,
        horizontal_flip=True,
        brightness_range=[0.9, 1.1]
    )

    # Il generatore si adatta ai dati di addestramento
    datagen.fit(X_train_normal)

    print("Inizio addestramento del modello con Data Augmentation...")
    # Addestramento con il generatore di dati
    history = autoencoder.fit(datagen.flow(X_train_normal, X_train_normal, batch_size=16),
                            steps_per_epoch=len(X_train_normal) / 16,
                            epochs=100,
                            validation_data=(X_train_normal, X_train_normal)) # Usa i dati originali per la validazione
    print("Addestramento completato.")

    # 4. Valutazione e Soglia
    X_test_normal = prepare_data(test_dir_normal)
    X_test_anomalies = prepare_data(test_dir_anomalies)

    if X_test_normal.size > 0 and X_test_anomalies.size > 0:
        reconstructed_normal = autoencoder.predict(X_test_normal)
        reconstructed_anomalies = autoencoder.predict(X_test_anomalies)

        # Calcola l'errore di ricostruzione (MSE)
        mse_normal = np.mean(np.power(X_test_normal - reconstructed_normal, 2), axis=(1, 2, 3))
        mse_anomalies = np.mean(np.power(X_test_anomalies - reconstructed_anomalies, 2), axis=(1, 2, 3))

        print(f"\nErrore MSE medio per immagini normali: {np.mean(mse_normal):.6f}")
        print(f"Errore MSE medio per immagini anomale: {np.mean(mse_anomalies):.6f}")

        # percentile della distribuzione degli errori normali
        threshold = np.percentile(mse_normal, 95) # Usa il 95° percentile come soglia
        print(f"Soglia di anomalia MSE (95° percentile): {threshold:.6f}")
        
        # Visualizzazione per capire la distribuzione dell'errore
        plt.figure(figsize=(10, 6))
        plt.hist(mse_normal, bins=50, alpha=0.7, label='Normali')
        plt.hist(mse_anomalies, bins=50, alpha=0.7, label='Anomale')
        plt.axvline(threshold, color='r', linestyle='dashed', linewidth=2, label='Soglia')
        plt.xlabel('Errore di ricostruzione (MSE)')
        plt.ylabel('Frequenza')
        plt.title('Distribuzione degli errori di ricostruzione')
        plt.legend()
        plt.show()
        
    # 5. Conversione in .tflite
    print("Conversione del modello in formato TFLite...")
    converter = tf.lite.TFLiteConverter.from_keras_model(autoencoder)
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS
    ]
    tflite_model = converter.convert()

    with open('anomaly_detector_improved.tflite', 'wb') as f:
        f.write(tflite_model)
        
    print("Modello convertito e salvato come anomaly_detector_improved.tflite")
