FROM archlinux:20200505

RUN pacman -Syu --noconfirm
RUN pacman -S jdk8-openjdk firefox chromium ffmpeg tmux git xorg-server-xvfb --noconfirm


# install lein
RUN curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > /usr/local/bin/lein; \
    chmod +x /usr/local/bin/lein

# setup recording stuff
COPY scripts/record /usr/local/bin/record

# start xvfb automatically to avoid needing to express in circle.yml
ENV DISPLAY :99
RUN printf '#!/bin/sh\nXvfb :99 -screen 0 1280x1024x24 &\nexec "$@"\n' > /tmp/entrypoint \
  && chmod +x /tmp/entrypoint \
        && mv /tmp/entrypoint /docker-entrypoint.sh

WORKDIR /tmp/
LABEL com.circleci.preserve-entrypoint=true
ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["bin/sh"]
