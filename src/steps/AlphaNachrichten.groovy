package steps

class AlphaNachrichten implements Serializable {
  def steps
  AlphaNachrichten(steps) {this.steps = steps}
  def docker(args) {
    steps.sh "./docker ${args}"
  }
}