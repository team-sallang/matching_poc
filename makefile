# k6 부하 테스트 실행
# 사용 전 확인사항:
# 1. Spring Boot 애플리케이션이 localhost:8080에서 실행 중이어야 합니다
# 2. .env 파일이 있으면 환경변수를 로드합니다 (없으면 env.example 참고)
# 3. Windows에서 make가 없으면: docker compose run --rm k6 run /scripts/matching.js 직접 실행
# 4. 테스트 사용자 파일이 없으면 k6 스크립트가 자동으로 메모리에 생성합니다
# 5. generate-users는 선택사항입니다 (파일이 있으면 로드, 없으면 메모리 생성)
test:
	docker compose run --rm k6 run /scripts/matching.js

# 테스트 사용자 파일 생성 (Node.js 스크립트 사용)
# Docker를 사용하여 Node.js 스크립트 실행 (로컬에 Node.js가 없어도 됨)
# 파일이 없으면 자동 생성, 있으면 건너뜀
# .env 파일이 있으면 VU 환경변수를 자동으로 로드

# 테스트 사용자 파일 강제 재생
regenerate-users:
	@echo "Force regenerating test users file using Docker..."
	@if [ -f .env ]; then \
		echo "Loading environment variables from .env file..."; \
		docker run --rm --env-file .env -v "$(PWD)/scripts/k6:/scripts" -w /scripts node:20-alpine node generate-users.js --force; \
	else \
		echo "No .env file found, using default VU=1000"; \
		docker run --rm -e VU=$${VU:-1000} -v "$(PWD)/scripts/k6:/scripts" -w /scripts node:20-alpine node generate-users.js --force; \
	fi

help:
	@echo "사용 가능한 명령어:"
	@echo "  make test            - k6 부하 테스트 실행 (사용자 파일 자동 생성)"
	@echo "  make generate-users  - 테스트 사용자 파일 생성 (파일이 없을 때만)"
	@echo "  make regenerate-users - 테스트 사용자 파일 강제 재생성"
	@echo ""
	@echo "주의사항:"
	@echo "  - Spring Boot 앱이 localhost:8080에서 실행 중이어야 합니다"
	@echo "  - .env 파일이 있으면 환경변수를 자동으로 로드합니다"
	@echo "  - 테스트 사용자 파일이 없으면 자동으로 생성됩니다"
	@echo "  - Windows에서 make가 없으면 docker compose 명령어를 직접 사용하세요"
