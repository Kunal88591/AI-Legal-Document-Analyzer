from flask import Flask, request, jsonify
import spacy

app = Flask(__name__)
nlp = spacy.load("en_core_web_sm")

@app.route('/analyze', methods=['POST'])
def analyze():
    text = request.json.get('text', '')
    doc = nlp(text)
    entities = [{"text": ent.text, "label": ent.label_} for ent in doc.ents]
    return jsonify({
        "entities": entities,
        "summary": " ".join(text.split()[:100]) + "..."
    })

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
