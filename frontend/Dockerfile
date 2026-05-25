# Build stage
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
COPY tsconfig.json vite.config.ts index.html ./
COPY src ./src

RUN npm ci

ARG VITE_OIDC_AUTHORITY
ARG VITE_OIDC_CLIENT_ID
ARG VITE_OIDC_REDIRECT_URI
ARG VITE_OIDC_SCOPE
ARG VITE_OIDC_POST_LOGOUT_REDIRECT_URI
ARG VITE_API_BASE_URL=

RUN test -n "$VITE_OIDC_AUTHORITY" \
  && test -n "$VITE_OIDC_CLIENT_ID" \
  && test -n "$VITE_OIDC_REDIRECT_URI"
RUN npm run build

# Runtime stage
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

