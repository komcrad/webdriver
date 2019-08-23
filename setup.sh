#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd $DIR

install-software() {
  software="vim bash-completion git i3-wm i3status i3lock feh rxvt-unicode jdk8-openjdk dmenu brave xclip chromium"
  sudo pacman -Syu --noconfirm $software
  sudo cp clipboard /usr/lib/urxvt/perl/
}

place-configs () {
  cp -r .bashrc .bg.darkArch.png .vimrc .i3/ .Xresources .Xdefaults ~
}

setup-clojure () {
  curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > lein
  chmod +x lein
  sudo mv lein /usr/local/bin/
  yes | sudo lein downgrade 2.8.1
  lein -version
}

vim-plugins () {
  mkdir -p ~/.vim/autoload ~/.vim/bundle
  curl -LSso ~/.vim/autoload/pathogen.vim https://tpo.pe/pathogen.vim
  cd ~/.vim/bundle
  git clone https://github.com/tpope/vim-fireplace.git
  git clone https://github.com/ervandew/supertab.git
  cd $DIR
}

main () {
  install-software
  place-configs
  setup-clojure
  vim-plugins
}

main
