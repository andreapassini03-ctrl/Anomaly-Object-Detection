import os
import subprocess
import sys
import tensorflow as tf

def run_command(command, cwd=None):
    """Esegue un comando da terminale e gestisce gli errori."""
    print(f"Esecuzione: {' '.join(command)}")
    process = subprocess.run(command, check=True, cwd=cwd, text=True, capture_output=True)
    print(process.stdout)
    print(process.stderr)
    return process.stdout

def setup_environment():
    #Clona il repository YOLOv5 e installa le dipendenze.
    print("Preparazione dell'ambiente...")
    if not os.path.exists("yolov5"):
        run_command(["git", "clone", "https://github.com/ultralytics/yolov5.git"])
    
    os.chdir("yolov5")
    
    run_command([sys.executable, "-m", "pip", "install", "-r", "requirements.txt"])
    run_command([sys.executable, "-m", "pip", "install", "tensorflow-cpu==2.20.0"])
    
    print("Ambiente preparato con successo.")

def download_weights(weights_url, weights_file):
    """Scarica i pesi pre-addestrati se non esistono."""
    if not os.path.exists(weights_file):
        print(f"Scaricamento dei pesi da {weights_url}...")
        import urllib.request
        urllib.request.urlretrieve(weights_url, weights_file)
    else:
        print(f"Pesi '{weights_file}' già presenti.")

def export_to_saved_model(weights_file):
    """Esporta il modello PyTorch in formato TensorFlow SavedModel."""
    print("Passo 2: Esportazione del modello in SavedModel...")
    run_command([
        sys.executable, "export.py",
        "--weights", weights_file,
        "--include", "saved_model",
        "--img", "640",
        "--batch", "1",
        "--device", "cpu"
    ])
    print("Esportazione in SavedModel completata.")

def convert_to_tflite_fp16():
    # Converte il SavedModel in TFLite con quantizzazione FP16
    saved_dir = "yolov5s_saved_model"

    # TensorFlow Lite Converter API. 
    converter = tf.lite.TFLiteConverter.from_saved_model(saved_dir)

    # Quantization - API TensorFlow Model Optimization Toolkit
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_types = [tf.float16]
    tflite_model = converter.convert()
    
    tflite_file_path = "yolov5s-fp16.tflite"
    with open(tflite_file_path, "wb") as f:
        f.write(tflite_model)
        
    print(f"Modello TFLite FP16 creato: '{tflite_file_path}'")
    return tflite_file_path

def main():
    """Funzione principale che orchestra tutti i passaggi."""
    try:
        setup_environment()
        
        weights_url = "https://github.com/ultralytics/yolov5/releases/download/v7.0/yolov5s.pt"
        weights_file = "yolov5s.pt"
        download_weights(weights_url, weights_file)
        
        export_to_saved_model(weights_file)
        
        tflite_file = convert_to_tflite_fp16()
        
        print(f"Il file '{tflite_file}' è stato salvato in '{os.path.abspath(os.path.join(os.getcwd(), tflite_file))}'.")
        print("Processo completato.")
    
    except subprocess.CalledProcessError as e:
        print(f"Un errore è avvenuto durante l'esecuzione di un comando: {e}")
        print(f"Output del comando: {e.stdout}")
        print(f"Errore del comando: {e.stderr}")
    except Exception as e:
        print(f"Si è verificato un errore inaspettato: {e}")

if __name__ == "__main__":
    main()



def convert_to_tflite_int8():
    # TensorFlow Lite Converter API
    converter = tf.lite.TFLiteConverter.from_saved_model(saved_dir)
    # Ottimizzazioni di quantizzazione
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    # Imposta la funzione per il representative dataset
    converter.representative_dataset = representative_dataset_gen
    # Check sul fatto che il modello possa usare operazioni su interi a 8 bit
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
    # Converti il modello
    tflite_model = converter.convert()

    tflite_file_path = "yolov5s-int8.tflite"
    with open(tflite_file_path, "wb") as f:
        f.write(tflite_model)

    print(f"Modello TFLite INT8 creato: '{tflite_file_path}'")
    return tflite_file_path






