from fastapi import FastAPI
from pydantic import BaseModel
from typing import List
import pickle
from sentence_transformers import SentenceTransformer, util

app = FastAPI()

model = SentenceTransformer("sentence-transformers/all-MiniLM-L6-v2")
with open("yoga_embeddings.pkl", "rb") as f:
    df = pickle.load(f)

class UserInput(BaseModel):
    age: int
    height: int
    weight: int
    goals: List[str]
    physical_issues: List[str]
    mental_issues: List[str]
    level: str

def recommend_asanas(user_profile):
    user_emb = {
        "goals": model.encode(" ".join(user_profile["goals"]), normalize_embeddings=True),
        "physical_issues": model.encode(" ".join(user_profile["physical_issues"]), normalize_embeddings=True),
        "mental_issues": model.encode(" ".join(user_profile["mental_issues"]), normalize_embeddings=True),
    }

    recommendations = []
    weights = {
        "goals_benefits": 4,
        "physical_benefits": 4,
        "mental_benefits": 4,
        "physical_match": 2,
        "mental_match": 2,
    }
    total_weight = sum(weights.values())

    for _, row in df.iterrows():
        score = 0.0
        contra_text = str(row["Contraindications"]).lower()

        discard = False
        for issue in user_profile["physical_issues"] + user_profile["mental_issues"]:
            issue = issue.lower()
            if issue in contra_text:
                discard = True
                break
            if util.cos_sim(model.encode(issue, normalize_embeddings=True), row["Contraindications_emb"]).item() > 0.25:
                discard = True
                break
        if discard:
            continue

        score += weights["goals_benefits"] * util.cos_sim(user_emb["goals"], row["Benefits_emb"]).item()
        score += weights["physical_benefits"] * util.cos_sim(user_emb["physical_issues"], row["Benefits_emb"]).item()
        score += weights["mental_benefits"] * util.cos_sim(user_emb["mental_issues"], row["Benefits_emb"]).item()

        score += weights["physical_match"] * util.cos_sim(user_emb["physical_issues"], row["Targeted Physical Problems_emb"]).item()
        score += weights["mental_match"] * util.cos_sim(user_emb["mental_issues"], row["Targeted Mental Problems_emb"]).item()

        score /= total_weight

        if score > 0:
            recommendations.append({
                "name": row["AName"],
                "score": round(score, 3),
                "benefits": row["Benefits"],
                "contraindications": row["Contraindications"]
            })

    recommendations = sorted(recommendations, key=lambda x: x["score"], reverse=True)
    return recommendations[:10]

@app.post("/recommend/")
async def get_recommendations(user_input: UserInput):
    user_profile = user_input.dict()
    recommended_asanas = recommend_asanas(user_profile)
    return {"recommended_asanas": recommended_asanas}
