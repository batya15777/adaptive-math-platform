from __future__ import annotations

import logging

from fastapi import APIRouter, Depends, HTTPException

from models.schemas import ClusteringRequest, ClusteringResponse
from services.clustering_service import ClusteringService

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/clustering", tags=["clustering"])


def get_clustering_service() -> ClusteringService:
    """DI hook — swap here for a cached/persisted model instance if needed later."""
    return ClusteringService()


@router.post("/run", response_model=ClusteringResponse)
def run_clustering(
    request: ClusteringRequest,
    service: ClusteringService = Depends(get_clustering_service),
) -> ClusteringResponse:
    """Cluster the supplied students and return user_id -> cluster_id assignments."""
    try:
        response = service.cluster(request)
        logger.info("Clustering done: %d students -> k=%d", len(request.students), response.k)
        return response
    except ValueError as exc:
        logger.warning("Rejected clustering request: %s", exc)
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:  # noqa: BLE001 — surface unexpected failures as 500 with context
        logger.exception("Clustering failed unexpectedly")
        raise HTTPException(status_code=500, detail=f"Clustering failed: {exc}") from exc
