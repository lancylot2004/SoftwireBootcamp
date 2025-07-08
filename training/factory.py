import sys
import base64
from pathlib import Path

PLACEHOLDER = "<REPLACE_WITH_BASE64_MODEL>"

def encode_onnx_to_py(onnx_path, template_path, output_py):
    onnx_path = Path(onnx_path)
    template_path = Path(template_path)

    if not onnx_path.exists():
        print(f"Error: ONNX model {onnx_path} does not exist.")
        sys.exit(1)

    if not template_path.exists():
        print(f"Error: Template file {template_path} does not exist.")
        sys.exit(1)

    with open(onnx_path, "rb") as f:
        encoded = base64.b64encode(f.read()).decode('ascii')

    with open(template_path, "r") as f:
        template = f.read()

    if PLACEHOLDER not in template:
        print(f"Error: Placeholder {PLACEHOLDER} not found in template.")
        sys.exit(1)

    result = template.replace(PLACEHOLDER, f"{encoded}")

    with open(output_py, "w") as f:
        f.write(result)

    print(f"Encoded {onnx_path} into {output_py}")

if __name__ == "__main__":
    if len(sys.argv) != 4:
        print("Usage: python factory.py <model.onnx> <template.py> <output.py>")
        sys.exit(1)

    encode_onnx_to_py(sys.argv[1], sys.argv[2], sys.argv[3])
