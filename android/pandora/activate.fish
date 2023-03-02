# this file is mean to be sourced.

cd (dirname (status --current-filename))

if not test -d venv
  /usr/bin/python3 -m venv venv
  source ./venv/bin/activate.fish
  ./install.sh
else
  source ./venv/bin/activate.fish
end