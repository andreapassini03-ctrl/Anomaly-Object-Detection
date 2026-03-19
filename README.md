# Anomaly-Object-Detection

Tesi & Mobile AI: Ricerca su modelli LiteRT e sviluppo di un'app Android nativa per Anomaly & Object Detection, con ottimizzazione di modelli `.tflite` tramite Python.


## Struttura del progetto

- **[`./TesiClassificazioneImmagini`](https://github.com/andreapassini03-ctrl/Anomaly-Object-Detection/tree/main/TesiClassificazioneImmagini)**: codice sorgente dell'app Android nativa per la classificazione e rilevamento anomalie/oggetti.
- **[`modelli/`](https://github.com/andreapassini03-ctrl/Anomaly-Object-Detection/tree/main/modelli)**: modelli TensorFlow Lite ottimizzati (`.tflite`).
- **[`scripts/`](https://github.com/andreapassini03-ctrl/Anomaly-Object-Detection/tree/main/scripts)**: script Python per ottimizzazione, addestramento e data augmentation dei modelli.
- **[`Presentazione Tesi.pdf`](https://github.com/andreapassini03-ctrl/Anomaly-Object-Detection/blob/main/Presentazione%20Tesi.pdf)**: presentazione della tesi.
- **[`TESI.pdf`](https://github.com/andreapassini03-ctrl/Anomaly-Object-Detection/blob/main/TESI.pdf)**: documento completo della tesi.


## How to run (Android)

Per eseguire l'applicazione su un telefono Android:

1. **Clona il repository**
	```sh
	git clone https://github.com/andreapassini03-ctrl/Anomaly-Object-Detection.git
	```

2. **Apri la cartella `TesiClassificazioneImmagini` in Android Studio**
	- File > Open > seleziona la cartella `TesiClassificazioneImmagini`.

3. **Collega il telefono Android via USB**
	- Attiva il debug USB nelle impostazioni sviluppatore del telefono.

4. **Compila e installa l'app**
	- Premi il pulsante "Run" in Android Studio.
	- L'app verrà installata e avviata automaticamente sul dispositivo.

5. **(Opzionale) Sostituisci/aggiorna i modelli `.tflite`**
	- Puoi aggiornare i modelli nella cartella `modelli/` e copiarli nella directory delle risorse dell'app se necessario.

**Requisiti:**
- Android Studio (ultima versione consigliata)
- Telefono Android con debug USB abilitato
- Permessi per installare app da USB
