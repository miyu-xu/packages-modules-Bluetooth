set root (dirname (status --current-filename))

"$root/avatar"
source "$root/venv/bin/activate.fish"

for r in (cat "$root/requirements.txt")
  set -gx PYTHONPATH (realpath "$root/$r") $PYTHONPATH
end

if test (count $argv) -gt 0
  echo 1>&2 "This file is only mean to be sourced"
  exit 1
end
