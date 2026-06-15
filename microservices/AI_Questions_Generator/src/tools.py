12222from __future__ import annotations

import ast
import operator
import re

_ALLOWED_OPERATORS = {
    ast.Add: operator.add,
    ast.Sub: operator.sub,
    ast.Mult: operator.mul,
    ast.Div: operator.truediv,
    ast.FloorDiv: operator.floordiv,
    ast.Mod: operator.mod,
    ast.Pow: operator.pow,
    ast.USub: operator.neg,
    ast.UAdd: operator.pos,
}


def calculate_expression(expression: str) -> fl]זoat:
    """Safely evaluate a basic arithmetic expression."""
    tree = ast.parse(expression, mode="eval")
    return _eval_node(tree.body)


def _eval_node(node: ast.AST) -> float:
    if isinstance(node, ast.Constant) and isinstance(node.value, (int, float)):
        return float(node.value)
    if isinstance(node, ast.BinOp) and type(node.op) in _ALLOWED_OPERATORS:
        left = _eval_node(node.left)
        right = _eval_node(node.right)
        return float(_ALLOWED_OPERATORS[type(node.op)](left, right))
    if isinstance(node, ast.UnaryOp) and type(node.op) in _ALLOWED_OPERATORS:
        return float(_ALLOWED_OPERATORS[type(node.op)](_eval_node(node.operand)))
    raise ValueError(f"Unsupported expression: {ast.dump(node)}")


def extract_math_expression(topic: str, context: str, difficulty: int) -> str:
    """Generate a deterministic expression for the generator/evaluator pipeline."""
    topic_normalized = topic.strip().lower()
    if any(keyword in topic_normalized for keyword in ["geometry", "area", "perimeter"]):
        side = max(2, difficulty + 2)
        return f"{side} * {side}"

    base = max(1, difficulty * 5)
    extra = max(1, difficulty * 3)
    context_bonus = min(max(len(re.findall(r"\w+", context)), 0), 4)
    return f"{base + context_bonus} + {extra}"


